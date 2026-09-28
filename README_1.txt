AUTO-DETECT VERSION 2.0

How it works:
1. Checks WiFi connected
2. Gets your phone IP e.g. 192.168.1.23
3. Scans 192.168.1.1 to 192.168.1.254 on port 5173 in parallel
4. First device that responds with HTTP 200-399 is loaded in WebView

No IP needed!

Build:
Upload to GitHub -> Actions -> Build APK

Requirements for PC:
npm run dev -- --host --port 5173
And firewall allow 5173