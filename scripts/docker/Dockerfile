FROM httpd:2.4.41

# Build arg
ARG HTDOCS=www
ARG USERNAME="apache"
ARG USERID="20001"
ARG GROUPNAME="apache"
ARG GROUPID="20001"

# Create execution user/group
RUN echo "Creatin group ${GROUPNAME}:${GROUPID}"
RUN groupadd -g ${GROUPID} ${GROUPNAME}
RUN echo "Creatin user ${USERNAME}:${USERID}"
RUN useradd -g ${GROUPNAME} -d /usr/local/apache2 -u ${USERID} ${USERNAME}
USER ${USERNAME}:${GROUPNAME}

# Copy files
COPY --chown=${USERNAME}:${GROUPNAME} ${HTDOCS} /usr/local/apache2/htdocs/

# Run
EXPOSE 80
CMD ["/usr/local/apache2/bin/apachectl","-DFOREGROUND"]
