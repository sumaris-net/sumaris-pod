FROM httpd:2.4.41
ARG HTDOCS=www

# copy statics in apache tree view
COPY ${HTDOCS} /usr/local/apache2/htdocs/

# container user managment
ENV USERNAME="sumaris_user" \
    USERID="20001" \
    GROUPNAME="sumaris" \
    GROUPID="20001"

RUN groupadd -g $GROUPID $GROUPNAME \
    && useradd -g $GROUPNAME -u $USERID $USERNAME \
    && chown -R $USERNAME:$GROUPNAME /usr/local/apache2
# TODO : do we have to force user ?? (USER $USERNAME)

EXPOSE 80
CMD ["/usr/local/apache2/bin/apachectl","-DFOREGROUND"]
