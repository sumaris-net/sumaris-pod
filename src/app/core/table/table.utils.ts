import {AppTable} from "./table.class";
import {debounceTime} from "rxjs/operators";
import {Entity} from "../services/model/entity.model";
import {firstFalsePromise} from "../../shared/observables";

export class AppTableUtils {

  static async waitIdle<T extends Entity<T> = Entity<any>, F = any>(table: AppTable<T, F>) {

    if (!table || !table.dataSource) {
      throw Error("Invalid table. Missing table or table.dataSource")
    }

    await firstFalsePromise(table.dataSource.$busy.asObservable()
      .pipe(
        debounceTime(100), // if not started yet, wait
      ));

  }

}
