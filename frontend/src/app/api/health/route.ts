import { NextResponse } from 'next/server';

export async function GET() {
  try {
    // Basic health check - you can add more sophisticated checks here
    const healthData = {
      status: 'UP',
      timestamp: new Date().toISOString(),
      service: 'crm-frontend',
      version: process.env.npm_package_version || '1.0.0',
      environment: process.env.NODE_ENV || 'development',
      uptime: process.uptime(),
    };

    return NextResponse.json(healthData, { status: 200 });
  } catch (error) {
    const errorData = {
      status: 'DOWN',
      timestamp: new Date().toISOString(),
      service: 'crm-frontend',
      error: error instanceof Error ? error.message : 'Unknown error',
    };

    return NextResponse.json(errorData, { status: 503 });
  }
}