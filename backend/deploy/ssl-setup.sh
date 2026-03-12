#!/bin/bash
export HOME=/home/ec2-user
export DuckDNS_Token=972cae05-c80e-480a-9e00-b4b1f4dad34f

echo "=== SSL Setup: Starting ==="

# Install acme.sh if not already installed
if [ ! -f "$HOME/.acme.sh/acme.sh" ]; then
    echo "Installing acme.sh..."
    curl -fsSL https://get.acme.sh | sh -s email=coorgfrnd@gmail.com --nocron 2>&1 || true
fi

# Source acme.sh environment
export PATH="$HOME/.acme.sh:$PATH"
source "$HOME/.acme.sh/acme.sh.env" 2>/dev/null || true

if [ ! -f "$HOME/.acme.sh/acme.sh" ]; then
    echo "ERROR: acme.sh not found after install - skipping SSL setup"
    exit 0
fi

# Save token directly to account.conf so acme.sh always finds it
echo "export DuckDNS_Token='972cae05-c80e-480a-9e00-b4b1f4dad34f'" >> "$HOME/.acme.sh/account.conf"

echo "=== Issuing cert via DuckDNS DNS-01 ==="
"$HOME/.acme.sh/acme.sh" --issue --dns dns_duckdns -d revconnect.duckdns.org --force --dnssleep 120 --server letsencrypt 2>&1 || true

# Install cert to nginx if obtained - use sudo tee for permission
echo "=== Installing cert to nginx ==="
sudo mkdir -p /etc/nginx/ssl
if [ -f "$HOME/.acme.sh/revconnect.duckdns.org_ecc/fullchain.cer" ]; then
    sudo cp "$HOME/.acme.sh/revconnect.duckdns.org_ecc/fullchain.cer" /etc/nginx/ssl/fullchain.pem
    sudo cp "$HOME/.acme.sh/revconnect.duckdns.org_ecc/revconnect.duckdns.org.key" /etc/nginx/ssl/privkey.pem
    echo "=== Cert files copied manually ==="
else
    "$HOME/.acme.sh/acme.sh" --install-cert -d revconnect.duckdns.org \
        --cert-file /tmp/fullchain.pem \
        --key-file /tmp/privkey.pem 2>&1 || true
    sudo mv /tmp/fullchain.pem /etc/nginx/ssl/fullchain.pem 2>/dev/null || true
    sudo mv /tmp/privkey.pem /etc/nginx/ssl/privkey.pem 2>/dev/null || true
fi

# Configure nginx HTTPS only if cert files exist
if [ -f /etc/nginx/ssl/fullchain.pem ] && [ -f /etc/nginx/ssl/privkey.pem ]; then
    echo "=== Cert found - configuring nginx HTTPS ==="
    cat > /tmp/revconnect-ssl.conf << 'NGINXEOF'
server {
    listen 443 ssl;
    server_name revconnect.duckdns.org;
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    root /var/www/html/revconnect-ui/browser;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}
server {
    listen 80;
    server_name revconnect.duckdns.org;
    return 301 https://$host$request_uri;
}
NGINXEOF
    sudo cp /tmp/revconnect-ssl.conf /etc/nginx/conf.d/revconnect-ssl.conf
    sudo systemctl reload nginx 2>/dev/null || true
    echo "=== HTTPS configured successfully ==="
else
    echo "=== SSL cert not ready yet - skipping HTTPS nginx config ==="
fi
