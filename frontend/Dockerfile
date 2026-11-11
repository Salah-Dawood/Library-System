# Serves the BookNest web pages. The browser reaches them through the gateway (localhost:8080),
# which also sends the /api and /auth calls of these pages to the services.
FROM nginx:alpine
COPY frontend/index.html /usr/share/nginx/html/
COPY frontend/css /usr/share/nginx/html/css
COPY frontend/js /usr/share/nginx/html/js
EXPOSE 80
