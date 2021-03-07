package liboptishop

import (
	"bytes"
	"net/http"

	"github.com/unixpickle/essentials"
)

type Response struct {
	Code        int
	ContentType string
	Data        []byte
}

func HandleGet(url string) *Response {
	req, err := http.NewRequest("GET", url, nil)
	essentials.Must(err)
	wr := newResponseWriter()
	http.DefaultServeMux.ServeHTTP(wr, req)
	return wr.Response()
}

func HandlePost(url string, data []byte) *Response {
	req, err := http.NewRequest("POST", url, bytes.NewReader(data))
	essentials.Must(err)
	wr := newResponseWriter()
	http.DefaultServeMux.ServeHTTP(wr, req)
	return wr.Response()
}

type responseWriter struct {
	header http.Header
	body   bytes.Buffer
	code   int
}

func newResponseWriter() *responseWriter {
	return &responseWriter{header: http.Header{}}
}

func (r *responseWriter) Response() *Response {
	code := r.code
	if code == 0 {
		code = http.StatusOK
	}
	contentType := r.header.Get("content-type")
	if contentType == "" {
		contentType = "text/html"
	}
	return &Response{
		Code:        code,
		ContentType: contentType,
		Data:        r.body.Bytes(),
	}
}

func (r *responseWriter) Header() http.Header {
	return r.header
}

func (r *responseWriter) Write(b []byte) (int, error) {
	return r.body.Write(b)
}

func (r *responseWriter) WriteHeader(code int) {
	r.code = code
}
