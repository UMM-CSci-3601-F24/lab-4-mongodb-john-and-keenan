import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable} from 'rxjs';
import { environment } from '../../environments/environment';
import { Todo } from './todo';
import { map } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class TodoService {
  readonly todoUrl: string = `${environment.apiUrl}todos`;

  private readonly categoryKey = 'category';
  private readonly ownerKey = 'owner';
  private readonly bodyKey = 'body';
  private readonly statusKey = 'status';


  constructor(private httpClient: HttpClient) {
  }

  getTodoById(id: string): Observable<Todo> {
    return this.httpClient.get<Todo>(`${this.todoUrl}/${id}`);
  }

  getTodos(filters?: {
    status?: boolean;
    owner?: string;
    body?: string;
    category?: string;
  }): Observable<Todo[]> {
    let httpParams: HttpParams = new HttpParams();
    if (filters) {


      if (filters.owner) {
        httpParams = httpParams.set('owner', filters.owner);
      }
      if (filters.body) {
        httpParams = httpParams.set('body', filters.body);
      }
      if (filters.category) {
        httpParams = httpParams.set('category', filters.category);
      }
    }

    return this.httpClient.get<Todo[]>(this.todoUrl, {
      params: httpParams,
    });
  }


  filterTodos(todos: Todo[], filters: { owner?: string; category?: string; status?: boolean; body?: string}): Todo[] {

    let filteredTodos = todos;

    if (filters.owner !== undefined) {
      filters.owner = filters.owner.toLowerCase();
      filteredTodos = filteredTodos.filter(todo => todo.owner.toLowerCase().indexOf(filters.owner) !== -1);
    }


    if (filters.category !== undefined) {
      filters.category = filters.category.toLowerCase();
      filteredTodos = filteredTodos.filter(todo => todo.category.toLowerCase().indexOf(filters.category) !== -1);
    }

    if (filters.status !== undefined) {

      filteredTodos = filteredTodos.filter(todo => todo.status === filters.status);
    }

    if (filters.body !== undefined) {
      filters.body = filters.body.toLowerCase();
      filteredTodos = filteredTodos.filter(todo => todo.body.toLowerCase().indexOf(filters.body) !== -1);
    }

    return filteredTodos;
  }

  limitTodos(todos: Todo[], limit: number): Todo[] {
    if (limit < 0) {
      throw new Error("Limit must be a non-negative number.");
    }
    return todos.slice(0, limit);
    }

    addTodo(newTodo: Partial<Todo>): Observable<string> {
      return this.httpClient.post<{id: string}>(this.todoUrl, newTodo).pipe(map(res => res.id));
    }

    deleteTodo(todoId: string): Observable<{ id: string}> {
      return this.httpClient.delete<{id: string}>(`${this.todoUrl}/${todoId}`); // .pipe(map(res => res.id));
    }

}
