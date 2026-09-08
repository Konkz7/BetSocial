/**
 * Who gets the approval interface.
 *
 * Mirrors UserRole on the server: 0 ordinary, 1 superuser, 2 admin. The server
 * decides what a role may actually do - /superusers/** is gated there - so this
 * only chooses which screens to show. Getting it wrong opens the wrong interface;
 * it does not grant anybody anything.
 */
export const ROLE_USER = 0;
export const ROLE_SUPERUSER = 1;
export const ROLE_ADMIN = 2;

export const isPrivileged = (profile: any) =>
  profile?.user_role === ROLE_SUPERUSER || profile?.user_role === ROLE_ADMIN;
