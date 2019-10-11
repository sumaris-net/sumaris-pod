import { EventEmitter, Injectable } from '@angular/core';

export declare type ProgressMode = 'determinate' | 'indeterminate' | 'buffer' | 'query' | 'none';

@Injectable({providedIn: 'root'})
export class ProgressBarService {
    public onProgressChanged: EventEmitter<ProgressMode> =  new EventEmitter();

    private requestsRunning = 0;

    list(): number {
        return this.requestsRunning;
    }

    increase(): void {
      this.requestsRunning++;
      console.log("TODO query running: "  + this.requestsRunning);
      if (this.requestsRunning === 1) {
        console.log("TODO service mode - query");
          this.onProgressChanged.emit('query');
      }

    }

    decrease(): void {
      if (this.requestsRunning > 0) {
          this.requestsRunning--;
      }
      console.log("TODO query running: "  + this.requestsRunning);
      if (this.requestsRunning === 0) {
        console.log("TODO service mode - none");
        this.onProgressChanged.emit('none');
      }
    }
}
