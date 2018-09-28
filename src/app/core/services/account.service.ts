import { Injectable } from "@angular/core";
import { KeyPair, CryptoService } from "./crypto.service";
import { Account, Referential, UserSettings, toDateISOString, UserProfileLabel } from "./model";
import { Subject, Subscription } from "rxjs-compat";
import gql from "graphql-tag";
import { TranslateService } from "@ngx-translate/core";
import { Apollo } from "apollo-angular";
import { Storage } from '@ionic/storage';
import { FetchPolicy } from "apollo-client";

import { BaseDataService, DataService } from "./data-service.class";
import { ErrorCodes, ServerErrorCodes } from "./errors";
import { environment } from "../../../environments/environment";

import { Validators, ValidatorFn } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";

const base58 = require('../../../lib/base58')

export declare interface AccountHolder {
  loaded: boolean;
  keypair: KeyPair;
  authToken: string;
  pubkey: string;
  account: Account;
  localSettings: {
    pages?: any
  };
  // TODO : use this ?
  mainProfile: String;
};
export interface AuthData {
  username: string;
  password: string;
}
export interface RegisterData extends AuthData {
  account: Account;
}
export interface AccountFieldDef<T = any, F = { searchText?: string; }> {
  name: string;
  label: string;
  required: boolean;
  dataService?: DataService<T, F>,
  dataServiceOptions?: any
}

const PRIORITIZED_USER_PROFILES: UserProfileLabel[] = ['ADMIN', 'SUPERVISOR', 'USER', 'GUEST'];

const TOKEN_STORAGE_KEY = "token"
const PUBKEY_STORAGE_KEY = "pubkey"
const SECKEY_STORAGE_KEY = "seckey"
const ACCOUNT_STORAGE_KEY = "account"
const SETTINGS_STORAGE_KEY = "settings"

/* ------------------------------------
 * GraphQL queries
 * ------------------------------------*/
// Get account query
const AccountQuery: any = gql`
  query Account($pubkey: String){
    account(pubkey: $pubkey){
      id
      firstName
      lastName
      email
      pubkey
      avatar
      statusId
      updateDate
      creationDate
      profiles
      settings {
        id
        locale
        latLongFormat
        content
        nonce
        updateDate
      }
      department {
        id
        label
        name
      }
    }
  }
`;
export declare type AccountVariables = {
  pubkey: string;
}
export declare type AccountResult = {
  account: Account;
}

// Check email query
const IsEmailExistsQuery: any = gql`
  query IsEmailExists($email: String, $hash: String){
    isEmailExists(email: $email, hash: $hash)
  }
`;
export declare type IsEmailExistsVariables = {
  email: string;
  hash: string;
}

// Save (create or update) account mutation
const SaveAccountMutation: any = gql`
  mutation SaveAccount($account:AccountVOInput){
    saveAccount(account: $account){
      id
      firstName
      lastName
      email
      pubkey
      avatar
      statusId
      updateDate
      creationDate
      profiles
      settings {
        id
        locale
        latLongFormat
        content
        nonce
        updateDate
      }
      department {
        id
      }
    }
  }
`;

// Sent confirmation email
const SendConfirmEmailMutation: any = gql`
  mutation sendAccountConfirmationEmail($email:String, $locale:String){
    sendAccountConfirmationEmail(email: $email, locale: $locale)
  }
`;

// Confirm account email
const ConfirmEmailMutation: any = gql`
  mutation confirmAccountEmail($email:String, $code:String){
    confirmAccountEmail(email: $email, code: $code)
  }
`;


// Subscription TEST
const TestSubscription: any = gql`
  subscription updateTrip($tripId: Int){
    updateTrip(tripId: $tripId)
  }
`;

// Authentication  query
const AuthQuery: any = gql`
  query Auth($token: String){
    authenticate(token: $token)
  }
`;

// New auth challenge query
const AuthChallengeQuery: any = gql`
  query AuthChallenge{
    authChallenge{
      challenge
      pubkey
      signature
    }
  }
`;

@Injectable()
export class AccountService extends BaseDataService {

  private data: AccountHolder = {
    loaded: false,
    keypair: null,
    authToken: null,
    pubkey: null,
    mainProfile: null,
    account: null,
    localSettings: null
  };

  private _startPromise: Promise<any>;
  private _started: boolean = false;
  private _additionalAccountFields: AccountFieldDef[] = [];

  public onLogin: Subject<Account> = new Subject<Account>();
  public onLogout: Subject<any> = new Subject<any>();
  public onAuthTokenChange: Subject<any> = new Subject<string | undefined>();

  public get account(): Account {
    return this.data.loaded ? this.data.account : undefined;
  }

  constructor(
    protected apollo: Apollo,
    private translate: TranslateService,
    private cryptoService: CryptoService,
    private storage: Storage
  ) {
    super(apollo);

    this.resetData();

    // Restoring local settings
    this._startPromise = this.restoreLocally()
      .then((account) => {
        this._started = true;
        if (account) this.onLogin.next(this.data.account);
      });

    this._debug = true;
  }

  private resetData() {
    this.data.loaded = false;
    this.data.keypair = null;
    this.data.authToken = null;
    this.data.pubkey = null;
    this.data.mainProfile = null;
    this.data.account = new Account();
    this.data.localSettings = null;
  }

  public isStarted(): boolean {
    return this._started;
  }

  public waitStart(): Promise<void> {
    if (this._started) return Promise.resolve();
    return this._startPromise;
  }

  public isLogin(): boolean {
    return !!(this.data.pubkey && this.data.loaded);
  }

  public isAuth(): boolean {
    return !!(this.data.pubkey && this.data.keypair && this.data.keypair.secretKey);
  }

  public hasProfile(label: string): boolean {
    if (!this.data.account || !this.data.account.pubkey) return false;

    return this.data.account.profiles && this.data.account.profiles.filter(up => up.label == label).length > 0;
  }

  public isAdmin(): boolean {
    return this.hasProfile("Administrator");
  }

  public async register(data: RegisterData): Promise<Account> {
    if (this.isLogin()) {
      throw new Error("User already login. Please logout before register.");
    }
    if (!data.username || !data.username) throw new Error("Missing required username por password");

    console.debug('[account] Register new user account...', data.account);
    this.data.loaded = false;
    let now = new Date();

    try {
      const keypair = await this.cryptoService.scryptKeypair(data.username, data.password);
      data.account.pubkey = base58.encode(keypair.publicKey);

      // Default values
      data.account.settings.locale = data.account.settings.locale || this.translate.currentLang || this.translate.defaultLang;
      data.account.settings.latLongFormat = environment.defaultLatLongFormat || 'DDMM';

      // TODO: add department to register form
      data.account.department.id = data.account.department.id || environment.defaultDepartmentId;

      this.data.keypair = keypair;
      const account = await this.saveAccount(data.account, keypair);

      // Default values
      account.avatar = account.avatar || "../assets/img/person.png";
      this.data.mainProfile = this.getMainProfile(account.profiles);

      this.data.account = account;
      this.data.pubkey = account.pubkey;
      this.data.loaded = true;

      await this.saveLocally();

      console.debug("[account] Account sucessfully registered in " + (new Date().getTime() - now.getTime()) + "ms");
      this.onLogin.next(this.data.account);
      return this.data.account;
    }
    catch (error) {
      console.error(error);
      this.resetData();
      throw error;
    };
  }

  async login(data: AuthData): Promise<Account> {
    if (!data.username || !data.username) throw "Missing required username por password";

    console.debug("[account] Trying to login...");

    let keypair;
    try {
      keypair = await this.cryptoService.scryptKeypair(data.username, data.password);
    } catch (error) {
      console.error(error);
      this.resetData();
      throw { code: ErrorCodes.UNKNOWN_ERROR, message: "ERROR.SCRYPT_ERROR" };
    }

    // Store pubkey+keypair
    this.data.pubkey = base58.encode(keypair.publicKey);
    this.data.keypair = keypair;

    // Load account data
    try {
      await this.loadData();
    }
    catch (err) {
      // If account not found, check if email is valid
      if (err && err.code == ErrorCodes.LOAD_ACCOUNT_ERROR) {

        let isEmailExists;
        try {
          isEmailExists = await this.isEmailExists(data.username);
        } catch (otherError) {
          throw err; // resend the first error
        }

        // Email not exists (no account)
        if (!isEmailExists) {
          throw { code: ErrorCodes.UNKNOWN_ACCOUNT_EMAIL, message: "ERROR.UNKNOWN_ACCOUNT_EMAIL" };
        }
        // Email exists, so error = 'bad password' 
        throw { code: ErrorCodes.BAD_PASSWORD, message: "ERROR.BAD_PASSWORD" };
      }

      throw err; // resend the first error
    }

    try {
      // Try to auth on remote server
      this.data.authToken = await this.authenticateAndGetToken();

      // Store to local storage
      await this.saveLocally();
    }
    catch (error) {
      console.error(error);
      this.resetData();
      throw error;
    }


    console.debug("[account] Sucessfully authenticated {" + this.data.pubkey.substr(0, 6) + "}");

    // Emit event to observers
    this.onLogin.next(this.data.account);

    return this.data.account;
  }

  public refresh(): Promise<Account> {
    if (!this.data.pubkey) return Promise.reject("User not logged");

    const locale = this.translate.currentLang;

    return this.loadData()
      .then(() => {
        return this.saveLocally();
      })
      .then(() => {
        console.debug("[account] Sucessfully reload account");
        this.onLogin.next(this.data.account);
        return this.data.account;
      });
  }

  loadData(): Promise<Account> {
    if (!this.data.pubkey) return Promise.reject("User not logged");

    this.data.loaded = false;


    return this.loadAccount(this.data.pubkey)
      .then((account) => {
        account = account || new Account();

        // Default values
        account.avatar = account.avatar || "../assets/img/person.png";
        account.settings = account.settings || new UserSettings();
        account.settings.locale = account.settings.locale || this.translate.currentLang;
        account.settings.latLongFormat = account.settings.latLongFormat || 'DDMM';

        // Read main profile
        this.data.mainProfile = this.getMainProfile(account.profiles);

        if (this.data.account) {
          account.copy(this.data.account);
        }
        else {
          this.data.account = account;
        }
        this.data.loaded = true;
        return this.data.account;
      })
      .catch((error) => {
        this.resetData();
        if (error.code && error.message) throw error;

        console.error(error);
        throw {
          code: ErrorCodes.LOAD_ACCOUNT_ERROR,
          message: 'ERROR.LOAD_ACCOUNT_ERROR'
        };
      });
  }

  public async restoreLocally(): Promise<Account | undefined> {

    // Restore from storage
    const values = await Promise.all([
      this.storage.get(PUBKEY_STORAGE_KEY),
      this.storage.get(TOKEN_STORAGE_KEY),
      this.storage.get(ACCOUNT_STORAGE_KEY),
      this.storage.get(SETTINGS_STORAGE_KEY),
      this.storage.get(SECKEY_STORAGE_KEY)
    ])
    const pubkey = values[0];
    const token = values[1];
    const accountStr = values[2];
    const settingsStr = values[3];
    const seckey = values[4];

    // Restore local settings
    this.data.localSettings = settingsStr && JSON.parse(settingsStr) || {};

    // Quit if no pubkey
    if (!pubkey) return;

    // Quit if could not auth on remote server
    const canRemoteAuth = !!token || !!seckey;
    if (!canRemoteAuth) return;

    if (this._debug) console.debug("[account] Restoring account {" + pubkey.substr(0, 6) + "}'...");

    this.data.pubkey = pubkey;
    this.data.keypair = seckey && {
      publicKey: base58.decode(pubkey),
      secretKey: base58.decode(seckey)
    };

    try {
      this.data.authToken = await this.authenticateAndGetToken(token);
      if (!this.data.authToken) throw "Authentication failed";
    }
    catch (error) {
      console.error(error);
      this.resetData();
      return
    }

    // No account: stop here (= data not loaded)
    if (!accountStr) return;

    let accountObj: any = JSON.parse(accountStr);
    if (!accountObj) return;

    let account = Account.fromObject(accountObj);
    if (account.pubkey != pubkey) return;

    this.data.account = account;
    this.data.mainProfile = this.getMainProfile(account.profiles);
    this.data.loaded = true;

    return account;
  }

  /** 
  * Save account into the local storage
  */
  async saveLocally(): Promise<void> {
    if (!this.data.pubkey) throw "User not logged";

    if (this._debug) console.debug("[account] Saving account {" + this.data.pubkey.substring(0, 6) + "} in local storage...");

    let copy = this.data.account.asObject();
    const seckey = this.data.keypair && !!this.data.keypair.secretKey && base58.encode(this.data.keypair.secretKey) || null;

    await Promise.all([
      this.storage.set(PUBKEY_STORAGE_KEY, this.data.pubkey),
      this.storage.set(TOKEN_STORAGE_KEY, this.data.authToken),
      this.storage.set(ACCOUNT_STORAGE_KEY, JSON.stringify(copy)),
      this.storage.set(SECKEY_STORAGE_KEY, seckey)
    ]);

    if (this._debug) console.debug("[account] Account saved in local storage");
  }

  /**
   * Create or update an user account, to the remote storage
   * @param account 
   * @param keyPair 
   */
  public async saveRemotely(account: Account): Promise<Account> {
    if (!this.data.pubkey) return Promise.reject("User not logged");
    if (this.data.pubkey != account.pubkey) return Promise.reject("Not user account");

    console.debug("[account] Saving account {" + account.pubkey.substring(0, 6) + "} remotely...");
    let now = new Date

    const updateAccount = await this.saveAccount(account, this.data.keypair);
    console.debug("[account] Account remotely saved in " + (new Date().getTime() - now.getTime()) + "ms");

    // Set default values
    account.avatar = account.avatar || "../assets/img/person.png";
    this.data.mainProfile = this.getMainProfile(account.profiles);

    this.data.account = account;
    this.data.loaded = true;
    // Save locally (in storage)
    await this.saveLocally();

    // Send event
    this.onLogin.next(this.data.account);

    return this.data.account;
  }

  public async logout(): Promise<void> {

    const tokenRemoved = !!this.data.authToken;

    this.resetData();

    await Promise.all([
      this.storage.remove(PUBKEY_STORAGE_KEY),
      this.storage.remove(TOKEN_STORAGE_KEY),
      this.storage.remove(ACCOUNT_STORAGE_KEY),
      this.storage.remove(SECKEY_STORAGE_KEY)
    ]);

    // Clear cache
    this.apollo.getClient().cache.reset();

    // Notify observers
    this.onLogout.next();
    if (tokenRemoved) {
      this.onAuthTokenChange.next(undefined);
    }

  }

  /**
   * Load a account by pubkey
   * @param pubkey 
   */
  public async loadAccount(pubkey: string, opts?: { fetchPolicy?: FetchPolicy }): Promise<Account | undefined> {

    if (this._debug) console.debug("[account-service] Loading account {" + pubkey.substring(0, 6) + "}...");
    var now = new Date();

    const res = await this.query<{ account: any }>({
      query: AccountQuery,
      variables: {
        pubkey: pubkey
      },
      error: { code: ErrorCodes.LOAD_ACCOUNT_ERROR, message: "ERROR.LOAD_ACCOUNT_ERROR" },
      fetchPolicy: opts && opts.fetchPolicy || environment.apolloFetchPolicy || undefined
    });

    if (res && res.account) {
      const account = new Account();
      account.fromObject(res.account);
      if (this._debug) console.debug("[account-service] Account {" + pubkey.substring(0, 6) + "} loaded in " + (new Date().getTime() - now.getTime()) + "ms", res);
      return account;
    }
    else {
      console.warn("[account-service] Account {" + pubkey.substring(0, 6) + "} not found !");
      return undefined;
    }
  }

  /**
   * Create or update an user account
   * @param account 
   * @param keyPair 
   */
  public async saveAccount(account: Account, keyPair: KeyPair): Promise<Account> {
    account.pubkey = account.pubkey || base58.encode(keyPair.publicKey);

    // First, try to get last account (for updateDate, etc)
    const existingAccount = await this.loadAccount(account.pubkey, { fetchPolicy: 'network-only' });
    if (!existingAccount || !existingAccount.updateDate) {
      throw { code: ErrorCodes.ACCOUNT_NOT_EXISTS, message: "ERROR.ACCOUNT_NOT_EXISTS" };
    }
    account.updateDate = existingAccount.updateDate || account.updateDate;
    if (account.settings && existingAccount.settings) {
      account.settings.updateDate = existingAccount.settings.updateDate || account.settings.updateDate;
    }

    const json = account.asObject();
    delete json.profiles;

    // Execute mutation
    const res = await this.mutate<{ saveAccount: any }>({
      mutation: SaveAccountMutation,
      variables: {
        account: json
      },
      error: {
        code: ErrorCodes.SAVE_ACCOUNT_ERROR,
        message: "ERROR.SAVE_ACCOUNT_ERROR"
      }
    });

    let data = res.saveAccount;

    // Copy update properties
    account.id = data.id;
    account.updateDate = data.updateDate;
    account.settings.id = data.settings && data.settings.id;
    account.settings.updateDate = data.settings && data.settings.updateDate;

    return account;
  }

  /**
   * Check if email is available for new account registration.
   * Throw an error if not available
   * @param email
   */
  public checkEmailAvailable(email: string): Promise<void> {

    return this.isEmailExists(email)
      .then(isEmailExists => {
        if (isEmailExists) {
          throw { code: ErrorCodes.EMAIL_ALREADY_REGISTERED, message: "ERROR.EMAIL_ALREADY_REGISTERED" };
        }
      });
  }

  /**
   * Check if email is exists in server.
   * @param email
   */
  async isEmailExists(email: string): Promise<boolean> {

    if (this._debug) console.debug("[account] Checking if {" + email + "} exists...");

    const data = await this.query<{ isEmailExists: boolean }, IsEmailExistsVariables>({
      query: IsEmailExistsQuery,
      variables: {
        email: email,
        hash: undefined
      }
    });

    if (this._debug) console.debug("[account] Email exist: " + (data && data.isEmailExists));

    return data && data.isEmailExists;
  }

  async sendConfirmationEmail(email: String, locale?: string): Promise<boolean> {

    locale = locale || this.translate.currentLang;
    console.debug("[trip-service] Sending confirmation email to {" + email + "} with locale {" + locale + "}...");

    return await this.mutate<boolean>({
      mutation: SendConfirmEmailMutation,
      variables: {
        email: email,
        locale: locale
      },
      error: {
        code: ErrorCodes.SENT_CONFIRMATION_EMAIL_FAILED,
        message: "ERROR.SENT_ACCOUNT_CONFIRMATION_EMAIL_FAILED"
      }
    });
  }

  async confirmEmail(email: String, code: String): Promise<boolean> {

    console.debug("[account-service] Sendng confirm request for email {" + email + "} with code {" + code + "}...");

    const res = await this.mutate<{ confirmAccountEmail: boolean }>({
      mutation: ConfirmEmailMutation,
      variables: {
        email: email,
        code: code
      },
      error: {
        code: ErrorCodes.CONFIRM_EMAIL_FAILED,
        message: "ERROR.CONFIRM_ACCOUNT_EMAIL_FAILED"
      }
    })
    return res && res.confirmAccountEmail;
  }

  public listenChanges(): Subscription {
    if (!this.data.pubkey) return Subscription.EMPTY;

    const self = this;

    console.debug('[account] [WS] Listening changes on {/subscriptions/websocket}...');

    const subscription = this.apollo.subscribe({
      query: gql`
        subscription updateAccount($pubkey: String, $interval: Int){
          updateAccount(pubkey: $pubkey, interval: $interval) {
            id
            updateDate
          }
        }`,
      variables: {
        pubkey: this.data.pubkey,
        interval: 10
      }
    }).subscribe({
      next({ data, errors }) {
        if (data && data.updateAccount) {
          const existingUpdateDate = self.data.account && toDateISOString(self.data.account.updateDate);
          if (existingUpdateDate !== data.updateAccount.updateDate) {
            console.debug("[account] [WS] Detected update on {" + data.updateDate + "}");
            self.refresh();
          }
        }
      },
      error(err) {
        if (err && err.code == ServerErrorCodes.NOT_FOUND) {
          console.log("[account] Account not exists anymore: force user to logout...", err);
          this.logout();
        }
        else {
          console.log("[account] [WS] Received error:", err);
        }
      },
      complete() {
        console.debug('[account] [WS] Completed');
      }
    });

    // Add log when closing WS
    subscription.add(() => console.debug('[account] [WS] Stop to listen changes'));

    return subscription;
  }

  public getPageSettings(pageId: string, propertyName?: string): string[] {
    const key = pageId.replace(/[/]/g, '__');
    return this.data.localSettings && this.data.localSettings.pages
      && this.data.localSettings.pages[key] && (propertyName && this.data.localSettings.pages[key][propertyName] || this.data.localSettings.pages[key]);
  }

  public async savePageSetting(pageId: string, value: any, propertyName?: string) {
    const key = pageId.replace(/[/]/g, '__');

    this.data.localSettings = this.data.localSettings || {};
    this.data.localSettings.pages = this.data.localSettings.pages || {}
    if (propertyName) {
      this.data.localSettings.pages[key] = this.data.localSettings.pages[key] || {};
      this.data.localSettings.pages[key][propertyName] = value;
    }
    else {
      this.data.localSettings.pages[key] = value;
    }

    // Update local settings
    await this.storeLocalSettings();
  }

  get additionalAccountFields(): AccountFieldDef[] {
    return this._additionalAccountFields;
  }

  getAdditionalAccountField(name: string): AccountFieldDef | undefined {
    return this._additionalAccountFields.find(f => f.name === name);
  }

  addAdditionalAccountField(field: AccountFieldDef) {
    if (!!this._additionalAccountFields.find(f => f.name === field.name)) {
      throw new Error("Additional account field {" + field.name + "} already define.");
    }
    if (this._debug) console.debug("[account] Adding additional account field {" + field.name + "}", field);
    this._additionalAccountFields.push(field);
  }

  async authenticateAndGetToken(token?: string, counter?: number): Promise<string> {
    if (!this.data.pubkey) throw "User not logged";

    if (this._debug && !counter) console.debug("[account] Authenticating on server...");

    if (counter > 4) {
      if (this._debug) console.debug(`[account] Authentification failed after ${counter} attempts`);
      throw { code: ErrorCodes.AUTH_SERVER_ERROR, message: "ERROR.AUTH_SERVER_ERROR" };
    }

    // Check if valid
    if (token) {
      const data = await this.query<{ authenticate: boolean }, { token: string }>({
        query: AuthQuery,
        variables: {
          token: token
        },
        error: {
          code: ErrorCodes.UNAUTHORIZED,
          message: "ERROR.UNAUTHORIZED"
        },
        fetchPolicy: 'network-only'
      });

      // Token is accepted by the server: store it
      if (data && data.authenticate) {
        this.onAuthTokenChange.next(token);
        return token; // return the token
      }

      // Continue: retry with another challenge
    }

    // Generate a new token
    const challengeError = {
      code: ErrorCodes.AUTH_CHALLENGE_ERROR,
      message: "ERROR.AUTH_CHALLENGE_ERROR"
    };
    const data = await this.query<{
      authChallenge: {
        pubkey: string,
        challenge: string,
        signature: string
      }
    }>({
      query: AuthChallengeQuery,
      variables: {},
      error: challengeError,
      fetchPolicy: 'network-only'
    });

    // Check challenge
    if (!data || !data.authChallenge) throw challengeError; // should never occur

    // TODO: check server signature

    // Do the challenge
    const signature = await this.cryptoService.sign(data.authChallenge.challenge, this.data.keypair);
    const newToken = `${this.data.pubkey}:${data.authChallenge.challenge}|${signature}`;

    // iIerate with the new token
    return await this.authenticateAndGetToken(newToken, (counter || 1) + 1 /* increment */);
  }

  /* -- Protected methods -- */

  private getMainProfile(profiles?: Referential[]): UserProfileLabel {

    if (this._debug) console.debug("[account] Retrieving user main profiles...", profiles);

    const mainProfile = profiles && profiles.length && PRIORITIZED_USER_PROFILES.find(label => !!profiles.find(p => p.label == label));
    const mainProfileLabel = (mainProfile && mainProfile['label'] || 'GUEST') as UserProfileLabel;

    if (this._debug) console.debug("[account] Main user profile {" + mainProfileLabel + "}");
    return mainProfileLabel;
  }

  private storeLocalSettings(): Promise<any> {
    console.debug("[account] Store local settings", this.data.localSettings);
    if (!this.data.localSettings) {
      return this.storage.remove(SETTINGS_STORAGE_KEY);
    }
    else {
      const settingsStr = JSON.stringify(this.data.localSettings);
      return this.storage.set(SETTINGS_STORAGE_KEY, settingsStr);
    }
  }

  public getValidators(field: AccountFieldDef): ValidatorFn | ValidatorFn[] {
    let validatorFns: ValidatorFn[] = [];
    if (field.required) {
      validatorFns.push(Validators.required);
    }
    if (!!field.dataService) {
      validatorFns.push(SharedValidators.entity);
    }

    return validatorFns.length ? Validators.compose(validatorFns) : validatorFns.length == 1 ? validatorFns[0] : undefined;
  }
}
