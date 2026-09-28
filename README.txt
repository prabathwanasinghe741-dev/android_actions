WifiWebChecker - How to use

IMPORTANT: Change the IP address!
1. Open app/src/main/java/com/example/wifiwebchecker/MainActivity.kt
2. Find line: private val TARGET_HOST = "192.168.1.100"
3. Change it to YOUR PC's IP (e.g. 192.168.1.5)
   - On Windows: run ipconfig in cmd
   - On Mac: ifconfig | grep inet

How to build WITHOUT Android Studio:
- Upload this whole folder to a new GitHub repo
- Go to Actions tab -> Build APK -> Run workflow
- Download APK from Artifacts (bottom of page)

App Logic:
- On open: checks TRANSPORT_WIFI
- If no WiFi -> shows "Please connect to WiFi"
- If WiFi -> tries to HEAD http://IP:5173 (timeout 3s)
- If reachable -> opens in WebView
- Else -> shows error with instructions + Retry button

For Vite/React dev server:
Make sure you run: npm run dev -- --host
So it listens on network, not just localhost.