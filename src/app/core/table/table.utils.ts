import {AppTable} from "./table.class";
import {debounceTime, filter, first} from "rxjs/operators";
import {Entity} from "../services/model/entity.model";

export class AppTableUtils {

  static async waitIdle<T extends Entity<T> = Entity<any>, F = any>(table: AppTable<T, F>) {

    if (!table || !table.dataSource) {
      throw Error("Invalid table. Missing table or table.dataSource")
    }

    await table.dataSource.$busy
      .pipe(
        debounceTime(100), // if not started yet, wait
        filter(loading => loading === false),
        first()
      ).toPromise();

  }

}
