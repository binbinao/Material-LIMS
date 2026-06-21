/**
 * Build-time constants for the frontend bundle.
 *
 * `BACKEND_VERSION` is updated manually whenever `pom.xml` is bumped.
 * `BUILD_DATE` is the date this frontend bundle was introduced; a future
 * CI step can replace it with `date -u +%Y-%m-%d` at build time.
 */

export const BACKEND_VERSION = '1.0.0-SNAPSHOT';
export const BUILD_DATE = '2026-06-21';