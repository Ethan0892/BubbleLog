# Discord Webhook Test Utility

This utility helps you test your Discord webhook configuration for BubbleLog.

## Quick Test Commands

### Using curl (Linux/macOS/Windows with curl installed)

```bash
# Basic test message
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{"content": "🧪 Test message from BubbleLog Discord integration!"}' \
     "YOUR_WEBHOOK_URL_HERE"
```

### Test Alert-Style Message
```bash
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{
       "embeds": [{
         "title": "⚡ Test Alert",
         "description": "This is a test of BubbleLog Discord webhook integration",
         "color": 16753920,
         "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
         "fields": [
           {
             "name": "Alert Type",
             "value": "Test Alert",
             "inline": true
           },
           {
             "name": "Severity",
             "value": "⚠️ Warning",
             "inline": true
           }
         ],
         "footer": {
           "text": "BubbleLog System Monitor • Test Message"
         }
       }]
     }' \
     "YOUR_WEBHOOK_URL_HERE"
```

### Test Status Report Style
```bash
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{
       "embeds": [{
         "title": "✅ Test Status Report",
         "description": "Sample system status report from BubbleLog",
         "color": 5763719,
         "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
         "fields": [
           {
             "name": "🖥️ CPU Usage",
             "value": "45.32%",
             "inline": true
           },
           {
             "name": "🧠 Memory Usage",
             "value": "65.2% (5.2 GB / 8.0 GB)",
             "inline": true
           },
           {
             "name": "👥 Players Online", 
             "value": "85 / 100 (85.0%)",
             "inline": true
           },
           {
             "name": "🏥 Overall Health",
             "value": "✅ Healthy",
             "inline": false
           }
         ],
         "footer": {
           "text": "BubbleLog System Monitor • Test Status Report"
         }
       }]
     }' \
     "YOUR_WEBHOOK_URL_HERE"
```

## Using PowerShell (Windows)

### Basic Test
```powershell
$webhookUrl = "YOUR_WEBHOOK_URL_HERE"
$body = @{
    content = "🧪 Test message from BubbleLog Discord integration!"
} | ConvertTo-Json

Invoke-RestMethod -Uri $webhookUrl -Method Post -Body $body -ContentType "application/json"
```

### Alert Test
```powershell
$webhookUrl = "YOUR_WEBHOOK_URL_HERE"
$body = @{
    embeds = @(
        @{
            title = "⚡ Test Alert"
            description = "This is a test of BubbleLog Discord webhook integration"
            color = 16753920
            timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
            fields = @(
                @{
                    name = "Alert Type"
                    value = "Test Alert"
                    inline = $true
                },
                @{
                    name = "Severity"
                    value = "⚠️ Warning"
                    inline = $true
                }
            )
            footer = @{
                text = "BubbleLog System Monitor • Test Message"
            }
        }
    )
} | ConvertTo-Json -Depth 4

Invoke-RestMethod -Uri $webhookUrl -Method Post -Body $body -ContentType "application/json"
```

## Using Python

```python
import requests
import json
from datetime import datetime

def test_webhook(webhook_url):
    # Basic test
    basic_payload = {
        "content": "🧪 Test message from BubbleLog Discord integration!"
    }
    
    response = requests.post(webhook_url, json=basic_payload)
    print(f"Basic test response: {response.status_code}")
    
    # Alert test
    alert_payload = {
        "embeds": [{
            "title": "⚡ Test Alert",
            "description": "This is a test of BubbleLog Discord webhook integration",
            "color": 16753920,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "fields": [
                {
                    "name": "Alert Type",
                    "value": "Test Alert",
                    "inline": True
                },
                {
                    "name": "Severity", 
                    "value": "⚠️ Warning",
                    "inline": True
                }
            ],
            "footer": {
                "text": "BubbleLog System Monitor • Test Message"
            }
        }]
    }
    
    response = requests.post(webhook_url, json=alert_payload)
    print(f"Alert test response: {response.status_code}")

# Usage
webhook_url = "YOUR_WEBHOOK_URL_HERE"
test_webhook(webhook_url)
```

## Using Node.js

```javascript
const fetch = require('node-fetch');

async function testWebhook(webhookUrl) {
    // Basic test
    const basicPayload = {
        content: "🧪 Test message from BubbleLog Discord integration!"
    };
    
    let response = await fetch(webhookUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(basicPayload)
    });
    
    console.log(`Basic test response: ${response.status}`);
    
    // Alert test
    const alertPayload = {
        embeds: [{
            title: "⚡ Test Alert",
            description: "This is a test of BubbleLog Discord webhook integration",
            color: 16753920,
            timestamp: new Date().toISOString(),
            fields: [
                {
                    name: "Alert Type",
                    value: "Test Alert", 
                    inline: true
                },
                {
                    name: "Severity",
                    value: "⚠️ Warning",
                    inline: true
                }
            ],
            footer: {
                text: "BubbleLog System Monitor • Test Message"
            }
        }]
    };
    
    response = await fetch(webhookUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(alertPayload)
    });
    
    console.log(`Alert test response: ${response.status}`);
}

// Usage
const webhookUrl = "YOUR_WEBHOOK_URL_HERE";
testWebhook(webhookUrl);
```

## Expected Responses

### Success (200 OK)
- The webhook should respond with HTTP status 200
- Messages should appear in your Discord channel immediately
- No error message in the response body

### Common Error Codes

#### 400 Bad Request
- Malformed JSON payload
- Invalid embed structure
- Check your JSON syntax

#### 404 Not Found  
- Webhook URL is incorrect
- Webhook has been deleted
- Recreate the webhook and update the URL

#### 429 Too Many Requests
- Rate limit exceeded (30 requests per minute)
- Wait before testing again
- Use longer delays between tests

## Webhook URL Format

Your webhook URL should look like:
```
https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789012345678
```

Components:
- Base URL: `https://discord.com/api/webhooks/`
- Webhook ID: Long numeric string (e.g., `123456789012345678`)
- Webhook Token: Long alphanumeric string

## Security Notes

- **Never share** your webhook URL publicly
- **Test responsibly** - don't spam the webhook
- **Use rate limiting** - Discord allows 30 requests per minute
- **Regenerate webhook** if URL is compromised

## Integration Testing

Once basic webhook testing works:

1. **Configure BubbleLog** with your webhook URL
2. **Enable alerts** with low thresholds for testing
3. **Trigger test alerts** by temporarily increasing system load
4. **Verify alert formatting** and content accuracy
5. **Test status reports** by enabling them with short intervals

## Troubleshooting Checklist

- [ ] Webhook URL is correct and complete
- [ ] Internet connection allows outbound HTTPS
- [ ] Firewall permits connections to Discord
- [ ] Webhook hasn't been deleted in Discord
- [ ] JSON payload is properly formatted
- [ ] Rate limits aren't being exceeded
- [ ] BubbleLog configuration syntax is correct

If tests pass but BubbleLog alerts don't work:
1. Check BubbleLog server logs for error messages
2. Verify alert thresholds aren't too high
3. Confirm monitoring is enabled for the alert types
4. Check alert cooldown settings

This testing utility helps ensure your Discord webhook integration is properly configured before deploying BubbleLog in production.
