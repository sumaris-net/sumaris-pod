# Building from source

Technologies: SUMARiS App is an Ionic 4 + Angular 7 App.

This article will explain how to install your environment, then build the application.

## Installation tools, and get sources

1. Install [Node.js](https://nodejs.org/en/) (v10)

2. Install global dependency: 
```bash
npm install -g ionic@5.4.5 cordova@9.0.0 cordova-res@0.8.1 native-run@0.2.9 
```

2. Get sources (clone the repo) : `git clone ...`

### Install additional tools (optional)
```bash
sudo apt-get install chromium-browser docker.io
```

## Web build

### For development and test

1. Install the environment:
```bash
cd sumaris-app/scrips
./env-global.sh
```

2. Check environment configuration:

   - Edit the file `src/environment/environment.ts`
   
3. Start the app
    ```bash
    cd sumaris-app
    npm start
    ```
   By default, the app should be accessible at [http://localhost:4200](http://localhost:4200)
   
   To change the default port, use this command instead:
    
    ```bash
    cd sumaris-app
    ng serve --port [port]
    ```

The application should be accessible at [localhost:4200](http://localhost:4200)

### Web build for production

1. Check environment configuration:

   - Edit the file `src/environment/environment-prod.ts`

2. Create the release
    ```bash
    npm run build --prod --release
    ```

## Android build 

### Build a debug APK, for development and test

```bash
cd scripts
./build-android.sh
```

### Build a release APK, for production

```bash
cd scripts
./release-android.sh
```

## Useful links

- Ionic 4 colors: https://www.joshmorony.com/a-primer-on-css-4-variables-for-ionic-4/
- Migration to Ionic 4 tips: https://www.joshmorony.com/my-method-for-upgrading-from-ionic-3-to-ionic-4/
- Signing Android APK: See doc at 
   https://www.c-sharpcorner.com/article/create-ionic-4-release-build-for-android/
