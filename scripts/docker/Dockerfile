FROM nginx:alpine

# Build arg
ARG HTML=www
ARG HTDOCS=www
ARG USERNAME="nginx"
ARG GROUPNAME="nginx"

# Create execution user/group
USER ${USERNAME}:${GROUPNAME}

# Copy files
COPY --chown=${USERNAME}:${GROUPNAME} ${HTML} /usr/share/nginx/html

EXPOSE 80
