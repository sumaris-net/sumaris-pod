# SUMARiS App

SUMARiS is a shared database for management of skate stocks.

Technologies: SUMARiS App is an Ionic 4 + Angular 5 App.

## Compile from source

1. Install [Node.js](https://nodejs.org/en/) (v10)

2. Install global dependency: 
```bash
npm install -g ionic cordova
```
3. Clone the repo: `git clone ...`
4. Install project dependencies
```bash
cd sumaris-app
npm install
```

5. Start the app
```
cd sumaris-app
npm start
```
or
```
ng serve --port [port]
```

The application should be accessible at [localhost:4200](http://localhost:4200)

6. Check environment configuration

Edit the file `src/environment/environment.ts`

7. Build a release
```
npm run build --prod --release
```


## Developer guide :

- Ionic 4 colors: https://www.joshmorony.com/a-primer-on-css-4-variables-for-ionic-4/
- Migration to Ionic 4 tips: https://www.joshmorony.com/my-method-for-upgrading-from-ionic-3-to-ionic-4/


## apt 
```
sudo apt-get install chromium-browser docker.io maven
```
