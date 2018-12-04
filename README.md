# SUMARiS App

SUMARiS is a shared database for management of skate stocks.

Technologies: SUMARiS App is an Ionic 4 + Angular 5 App.

## Compile from source

1. Install Node.js v8+
3. Install global dependencies: 
```
npm install -g ionic@^3.20.0 cordova@^8.0.0 @angular/cli@^1.7.4
```
3. Clone the repo: `git clone ...`
4. Install project dependencies
```
cd sumaris-app
npm install
```

5. Start the server (with GraphQL API). See project SUMARiS Pod.

A GraphQL editor should be accessible at [localhost:8080](http://localhost:8080/graphql)

6. Start app
```
cd sumaris-app
npm start
```
ou
```
ng serve --port [port]
```

The application should be accessible at [localhost:4200](http://localhost:4200)

7. Check environment configuration

Edit the file `src/environment/environment.ts`

8. Build a release
```
npm run build --prod --release
```


## Developer guide :

- Ionic 4 colors: https://www.joshmorony.com/a-primer-on-css-4-variables-for-ionic-4/
- Migration to Ionic 4 tips: https://www.joshmorony.com/my-method-for-upgrading-from-ionic-3-to-ionic-4/
