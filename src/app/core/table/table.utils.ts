import {AppTable} from "./table.class";
import {debounceTime, filter, first} from "rxjs/operators";
import {Entity} from "../services/model";

export class AppTableUtils {

  static async waitLoaded<T extends Entity<T> = any, F = any>(table: AppTable<T, F>) {

    if (!table || !table.dataSource) {
      throw Error("Invalid table. Missing table or table.dataSource")
    }

    await table.dataSource.onLoading
      .pipe(
        debounceTime(100), // if not started yet, wait
        filter(loading => !loading),
        first()
      ).toPromise();

  }

}
