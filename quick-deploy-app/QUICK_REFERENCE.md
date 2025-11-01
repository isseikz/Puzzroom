# Quick Reference: Firebase Functions with Kotlin

## Data Structure Sharing

### Defining Shared Models

Create models in `src/commonMain/kotlin/tokyo/isseikuzumaki/quickdeploy/data/`:

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class MyData(
    val id: String,
    val value: Int
)
```

### Using in Android/iOS

```kotlin
import tokyo.isseikuzumaki.quickdeploy.data.MyData
import kotlinx.serialization.json.Json

val data = MyData(id = "123", value = 42)
val json = Json.encodeToString(data)
// Send to Firebase or store in Firestore
```

### Using in Firebase Functions

```javascript
// functions/index.js
export const processData = onRequest(async (req, res) => {
  // Parse JSON from client (matches Kotlin structure)
  const data = req.body; // { "id": "123", "value": 42 }
  
  // Process
  const result = {
    id: data.id,
    value: data.value * 2
  };
  
  res.json(result);
});
```

## Common Patterns

### Pattern 1: HTTP Request/Response

**Client (Kotlin):**
```kotlin
@Serializable
data class Request(val input: String)

@Serializable
data class Response(val output: String)

// Send request
val request = Request(input = "hello")
val json = Json.encodeToString(request)
// POST to https://us-central1-<project-id>.cloudfunctions.net/myFunction
```

**Server (JavaScript):**
```javascript
export const myFunction = onRequest(async (req, res) => {
  const input = req.body.input;
  const output = input.toUpperCase();
  res.json({ output: output });
});
```

### Pattern 2: Firestore Trigger

**Client (Kotlin):**
```kotlin
@Serializable
data class UserMessage(
    val userId: String,
    val message: String,
    val timestamp: Long
)

// Write to Firestore
val message = UserMessage(
    userId = "user123",
    message = "Hello!",
    timestamp = System.currentTimeMillis()
)
firestore.collection("messages").add(message)
```

**Server (JavaScript):**
```javascript
export const onMessageCreate = onDocumentCreated(
  "messages/{messageId}",
  async (event) => {
    const data = event.data.data();
    const userId = data.userId;
    const message = data.message;
    
    // Process message
    await getFirestore()
      .collection("notifications")
      .add({
        userId: userId,
        text: `New message: ${message}`,
        timestamp: Date.now()
      });
  }
);
```

## Deployment Commands

```bash
# Local testing
cd functions
npm run serve

# Deploy all functions
npm run deploy

# Deploy specific function
firebase deploy --only functions:functionName

# View logs
npm run logs
```

## Testing

### Test HTTP Function
```bash
# Using curl
curl "http://localhost:5001/<project-id>/us-central1/makeUppercase?text=hello"

# Expected response
{"result":"HELLO"}
```

### Test Firestore Trigger
1. Start emulator: `npm run serve`
2. Open http://localhost:4000
3. Go to Firestore tab
4. Add document to `messages` collection
5. Check function logs in terminal

## TypeScript Definitions (Optional)

To generate TypeScript definitions from Kotlin models:

1. Use kotlin-js-plain-objects plugin
2. Or manually create .d.ts files:

```typescript
// types.d.ts
export interface Message {
  text: string;
  timestamp: number;
}

export interface MessageRequest {
  original: string;
}

export interface MessageResponse {
  result: string;
}
```

## Best Practices

1. **Version your data models**: Add version field for schema evolution
   ```kotlin
   @Serializable
   data class VersionedData(
       val version: Int = 1,
       val data: String
   )
   ```

2. **Validate inputs**: Always validate in Firebase Functions
   ```javascript
   if (!req.body.text || typeof req.body.text !== 'string') {
     res.status(400).send('Invalid input');
     return;
   }
   ```

3. **Handle errors gracefully**
   ```kotlin
   try {
       val data = Json.decodeFromString<MyData>(json)
       // Process
   } catch (e: SerializationException) {
       // Handle error
   }
   ```

4. **Use environment variables for config**
   ```bash
   firebase functions:config:set api.key="SECRET_KEY"
   ```
   ```javascript
   const apiKey = functions.config().api.key;
   ```

## Resources

- [Firebase Functions Docs](https://firebase.google.com/docs/functions)
- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Full Setup Guide](FIREBASE_SETUP.md)
