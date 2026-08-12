import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const code = searchParams.get('code');

  if (!code) {
    return NextResponse.json({ error: 'Authorization code missing in OAuth redirect' }, { status: 400 });
  }

  const apiKey = process.env.UPSTOX_API_KEY;
  const apiSecret = process.env.UPSTOX_API_SECRET;
  const redirectUri = process.env.UPSTOX_REDIRECT_URI || 'http://localhost:3000/api/auth/upstox/callback';

  if (!apiKey || !apiSecret) {
    return NextResponse.json({ error: 'Upstox API key or secret missing in server environment' }, { status: 500 });
  }

  try {
    const params = new URLSearchParams();
    params.append('code', code);
    params.append('client_id', apiKey);
    params.append('client_secret', apiSecret);
    params.append('redirect_uri', redirectUri);
    params.append('grant_type', 'authorization_code');

    const response = await fetch('https://api.upstox.com/v2/login/authorization/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Accept': 'application/json',
      },
      body: params.toString(),
    });

    const data = await response.json();

    if (response.ok && data.access_token) {
      // Redirect back to dashboard with token status banner flag
      const redirectUrl = new URL('/', request.url);
      redirectUrl.searchParams.set('token_status', 'success');
      return NextResponse.redirect(redirectUrl);
    } else {
      return NextResponse.json({ error: 'Failed to exchange authorization code for token', details: data }, { status: 400 });
    }
  } catch (err) {
    return NextResponse.json({ error: 'Internal server error exchanging Upstox OAuth token', details: String(err) }, { status: 500 });
  }
}
