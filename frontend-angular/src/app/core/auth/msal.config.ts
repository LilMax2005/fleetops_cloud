import {
  BrowserCacheLocation,
  InteractionType,
  IPublicClientApplication,
  LogLevel,
  PublicClientApplication,
} from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { environment } from '../../../environments/environment';

/** Authorization Code Flow + PKCE contra Microsoft Entra ID (sin client secret en el navegador). */
export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.entra.spaClientId,
      authority: `https://login.microsoftonline.com/${environment.entra.tenantId}`,
      redirectUri: environment.entra.redirectUri,
      postLogoutRedirectUri: environment.entra.redirectUri,
    },
    cache: { cacheLocation: BrowserCacheLocation.SessionStorage },
    system: {
      loggerOptions: {
        logLevel: environment.production ? LogLevel.Warning : LogLevel.Info,
        piiLoggingEnabled: false,
        loggerCallback: (level, message) => {
          if (level <= LogLevel.Warning) console.warn(message);
        },
      },
    },
  });
}

export const LOGIN_SCOPES = ['openid', 'profile', environment.entra.apiScope];

export function msalGuardConfigFactory(): MsalGuardConfiguration {
  return { interactionType: InteractionType.Redirect, authRequest: { scopes: LOGIN_SCOPES } };
}

/**
 * MsalInterceptor agrega "Authorization: Bearer <access token>" a toda llamada
 * cuya URL comience con alguno de estos prefijos (backend vía APIM).
 */
export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, string[]>();
  Object.values(environment.api).forEach((url) => protectedResourceMap.set(url, [environment.entra.apiScope]));
  return { interactionType: InteractionType.Redirect, protectedResourceMap };
}
