# SUMARiS App

SUMARiS is a shared database for management of skate stocks.

Technologies: SUMARiS App is an Ionic 4 + Angular 5 App.

## Compile from source

1. Install Node.js v8+
3. Install global dependencies: 
```
npm install -g ionic cordova
```
3. Clone the repo: `git clone ...`
4. Install project dependencies
```
cd sumaris-app
yarn
```

5. Start the server (with GraphQL API). See project SUMARiS Pod.

A GraphQL editor should be accessible at [localhost:8080](http://localhost:8080/graphql)

6. Start app
```
cd sumaris-app
ionic serve -l
```

The application should be accessible at [localhost:8100](http://localhost:8100)

7. Check environment configuration

Edit the file `src/lib/conf.js`

8. Build a release
```
npm run build --prod --release
```
