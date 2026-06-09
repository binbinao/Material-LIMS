# Frontend image: serves the pre-built dist via nginx and proxies /api to lims-backend.
# We assume dist/ is built on the host first (npm run build) to avoid OOM in containers
# with low-memory podman/docker machines.
FROM nginx:1.27-alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
