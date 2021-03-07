# optishop-android

This is an Android application for [Optishop](https://optishop.us). This application is just the original web app, packaged to run natively on-device. The motivation is that the web app may require too many setup steps (.g. bookmarking, creating an account, etc.), whereas a native application requires none of these steps. Additionally, a native application can trivially scale to many users, whereas a web application cannot. Furthermore, native applications have a natural discovery mechanism, and it will be easier to monetize in the future.

# How it works

The Optishop web application uses a [Go](https://golang.org/) backend and a JavaScript/HTML/CSS front-end. It's trivial to ship such front-ends on Android using a [WebView](https://developer.android.com/reference/android/webkit/WebView), but it's not obvious how to ship a Go server within the app.

To ship the Go server, I converted the original web server into a simple Go API. This API can then be compiled with [gomobile](https://pkg.go.dev/golang.org/x/mobile/cmd/gomobile) and used through a Java interface. This API provides the following functions, which expose server-like functionality without actually using sockets:

```
type Response struct {
	Code        int
	ContentType string
	Data        []byte
}

func HandleGet(url string) *Response

// Methods called to setup the Go server's database,
// session cookies on Target's website, etc.
func StartSetup(assetsDir, storageDir string)
func SetupComplete() bool
func SetupError() string 
```

The Android app uses a `WebView`, which provides simple functionality to override web requests. We can override web requests to `optishop.us` to instead call the Go API's `HandleGet()` method. This makes the Go API act as a web server for the `WebView`, without ever actually listening on a socket.

# Where is the code

Almost all of the Java code is in one file, [MainActivity.java](Optishop/app/src/main/java/com/aqnichol/optishop/MainActivity.java). This is the code for creating a `WebView` and communicating with the Go backend.

The web assets, such as the initial loading and error pages, are located in an [assets](Optishop/app/src/main/assets) directory.

The Go code is all in [liboptishop](liboptishop).
