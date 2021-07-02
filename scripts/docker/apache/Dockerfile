FROM httpd:2.4.41

# Build args
ARG HTDOCS=www
ARG USERNAME="www"
ARG USERID="20001"
ARG GROUPNAME="www"
ARG GROUPID="20001"

RUN groupadd -g $GROUPID $GROUPNAME
RUN useradd -g $GROUPNAME -d /usr/local/apache2 -u $USERID $USERNAME
RUN chown -R $USERNAME:$GROUPNAME /usr/local/apache2
USER $USERNAME:$GROUPNAME

# copy statics in apache tree view
COPY ${HTDOCS} /usr/local/apache2/htdocs/

EXPOSE 80
CMD ["/usr/local/apache2/bin/apachectl","-DFOREGROUND"]
