'use client';

import { Component, ReactNode } from 'react';
import { AlertTriangle, Copy, Check, RefreshCw } from 'lucide-react';
import { useState } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
  traceId?: string;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    const traceId = typeof window !== 'undefined' 
      ? sessionStorage.getItem('app-trace-id') || 'unknown'
      : 'unknown';
    
    return {
      hasError: true,
      error,
      traceId,
    };
  }

  componentDidCatch(error: Error, errorInfo: any) {
    const { traceId } = this.state;
    
    // Log to console
    console.error(`[${traceId}] Error caught by boundary:`, error, errorInfo);
    
    // Send to monitoring service (optional)
    if (typeof window !== 'undefined' && process.env.NODE_ENV === 'production') {
      fetch('/api/errors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          traceId,
          message: error.message,
          stack: error.stack,
          componentStack: errorInfo.componentStack,
          timestamp: new Date().toISOString(),
          userAgent: navigator.userAgent,
        }),
      }).catch(console.error);
    }
  }

  render() {
    if (this.state.hasError) {
      return <ErrorDisplay error={this.state.error!} traceId={this.state.traceId!} />;
    }

    return this.props.children;
  }
}

function ErrorDisplay({ error, traceId }: { error: Error; traceId: string }) {
  const [copied, setCopied] = useState(false);

  const copyTraceId = () => {
    navigator.clipboard.writeText(traceId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 p-4">
      <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6">
        <div className="flex items-center space-x-3 mb-4">
          <AlertTriangle className="w-8 h-8 text-red-500" />
          <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
            Something went wrong
          </h2>
        </div>
        
        <p className="text-gray-600 dark:text-gray-400 mb-4">
          We're sorry, but something unexpected happened. Our team has been notified.
        </p>
        
        <div className="bg-gray-50 dark:bg-gray-700 rounded p-3 mb-4">
          <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">Error Message</div>
          <div className="font-mono text-sm text-gray-900 dark:text-gray-100">
            {error.message}
          </div>
        </div>
        
        <div className="bg-blue-50 dark:bg-blue-900/30 rounded p-3 mb-4">
          <div className="text-sm text-blue-600 dark:text-blue-400 mb-2 font-medium">
            Trace ID (for support)
          </div>
          <div className="flex items-center justify-between">
            <code className="text-sm font-mono text-blue-900 dark:text-blue-200">
              {traceId}
            </code>
            <button
              onClick={copyTraceId}
              className="ml-2 p-1 hover:bg-blue-100 dark:hover:bg-blue-800 rounded transition-colors"
              title="Copy trace ID"
            >
              {copied ? (
                <Check className="w-4 h-4 text-green-600 dark:text-green-400" />
              ) : (
                <Copy className="w-4 h-4 text-blue-600 dark:text-blue-400" />
              )}
            </button>
          </div>
        </div>
        
        <button
          onClick={() => window.location.reload()}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded flex items-center justify-center space-x-2 transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Reload Page</span>
        </button>
      </div>
    </div>
  );
}
