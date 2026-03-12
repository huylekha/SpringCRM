import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios, { AxiosInstance } from 'axios';
import MockAdapter from 'axios-mock-adapter';
import { setupInterceptors } from '../interceptors';

describe('Trace ID Interceptor', () => {
  let client: AxiosInstance;
  let mock: MockAdapter;
  
  beforeEach(() => {
    client = axios.create({ baseURL: 'http://localhost:8080' });
    mock = new MockAdapter(client);
    setupInterceptors(client);
    
    // Mock sessionStorage
    const sessionStorageMock = (() => {
      let store: Record<string, string> = {};
      return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => {
          store[key] = value;
        },
        clear: () => {
          store = {};
        },
      };
    })();
    
    Object.defineProperty(window, 'sessionStorage', {
      value: sessionStorageMock,
      writable: true,
    });
    
    sessionStorageMock.clear();
  });
  
  it('should add X-Trace-Id header to requests', async () => {
    mock.onGet('/test').reply((config) => {
      expect(config.headers?.['X-Trace-Id']).toBeDefined();
      expect(config.headers?.['X-Trace-Id']).toHaveLength(16);
      return [200, { data: 'test' }];
    });
    
    await client.get('/test');
  });
  
  it('should reuse the same trace ID across multiple requests', async () => {
    let firstTraceId: string | undefined;
    
    mock.onGet('/test1').reply((config) => {
      firstTraceId = config.headers?.['X-Trace-Id'];
      return [200, { data: 'test1' }];
    });
    
    mock.onGet('/test2').reply((config) => {
      expect(config.headers?.['X-Trace-Id']).toBe(firstTraceId);
      return [200, { data: 'test2' }];
    });
    
    await client.get('/test1');
    await client.get('/test2');
  });
  
  it('should store trace ID from response header', async () => {
    const serverTraceId = 'abc123def4567890';
    
    mock.onGet('/test').reply(200, { data: 'test' }, {
      'x-trace-id': serverTraceId,
    });
    
    await client.get('/test');
    
    expect(window.sessionStorage.getItem('app-trace-id')).toBe(serverTraceId);
  });
  
  it('should attach trace ID to error object', async () => {
    const serverTraceId = 'error-trace-id-1';
    
    mock.onGet('/error').reply(500, { error: 'Server error' }, {
      'x-trace-id': serverTraceId,
    });
    
    try {
      await client.get('/error');
    } catch (error: any) {
      expect(error.traceId).toBe(serverTraceId);
    }
  });
  
  it('should generate trace ID in hex format (16 characters)', async () => {
    mock.onGet('/test').reply((config) => {
      const traceId = config.headers?.['X-Trace-Id'];
      expect(traceId).toMatch(/^[0-9a-f]{16}$/);
      return [200, {}];
    });
    
    await client.get('/test');
  });
  
  it('should add trace ID to request even if sessionStorage is empty', async () => {
    window.sessionStorage.clear();
    
    mock.onGet('/test').reply((config) => {
      expect(config.headers?.['X-Trace-Id']).toBeDefined();
      expect(config.headers?.['X-Trace-Id']).toHaveLength(16);
      return [200, {}];
    });
    
    await client.get('/test');
  });
});
