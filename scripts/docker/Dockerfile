FROM nginx:alpine

# Build arg
ARG HTML=www
ARG HTDOCS=www
ARG USERNAME="apache"
ARG USERID="20001"
ARG GROUPNAME="apache"
ARG GROUPID="20001"

# Create execution user/group
RUN echo "Creatin group ${GROUPNAME}:${GROUPID}"
RUN groupadd -g ${GROUPID} ${GROUPNAME}
RUN echo "Creatin user ${USERNAME}:${USERID}"
RUN useradd -g ${GROUPNAME} -d /usr/share/nginx -u ${USERID} ${USERNAME}
USER ${USERNAME}:${GROUPNAME}

# Copy files
COPY --chown=${USERNAME}:${GROUPNAME} ${HTML} /usr/share/nginx/html

EXPOSE 80
