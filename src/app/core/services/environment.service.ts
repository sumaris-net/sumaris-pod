import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { shareReplay } from "rxjs/operators";
import { environment } from "src/environments/environment";
import { Environment } from "src/environments/environment.class";

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {

  protected _debug: boolean;

  private data = environment;
  private readonly configUrl = 'assets/config/config.json';

  get environment(): Environment {
    return this.data;
  }

  constructor(
    private http: HttpClient,
  ) {
    this._debug = !environment.production;
    if (this._debug) console.debug("[environment-service] Creating service");
  }

  load(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http
      .get<Environment>(`${this.configUrl}`)
      .pipe(shareReplay(1))
      .subscribe((environment) => {
        // overwite data with externals
        this.data = {...this.data, ...environment}
        if (this._debug) console.debug("[environment-service] External environment configuration loaded");
        resolve();
      });
    });
  }
}
