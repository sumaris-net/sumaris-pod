FROM httpd:2.4.41
ARG HTDOCS=www
ARG PORT=80

RUN apt-get update && apt-get -y install unzip

# copy statics and unzip in apache tree view
COPY ${HTDOCS}/* /usr/local/apache2/htdocs/

EXPOSE ${PORT}
CMD ["/usr/local/apache2/bin/apachectl","-DFOREGROUND"]
