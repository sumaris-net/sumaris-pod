import {BehaviorSubject, Observable, Subject} from "rxjs";
import {catchError, filter, first, tap} from "rxjs/operators";
import {isNotNil} from "./functions";


export function filterNotNil<T = any>(obs: Observable<T>): Observable<T> {
  return obs.pipe(filter(isNotNil));
}
export function firstNotNil<T = any>(obs: Observable<T>): Observable<T> {
  return obs.pipe(first(isNotNil));
}

export function firstNotNilPromise<T = any>(obs: Observable<T>): Promise<T> {
  return firstNotNil(obs).toPromise();
}
