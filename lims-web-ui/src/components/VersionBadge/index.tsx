import React from 'react';
import { Tag, Tooltip } from 'antd';
import { BACKEND_VERSION, BUILD_DATE } from '../../buildInfo';

/**
 * VersionBadge — small blue tag in the top-right header showing the
 * frontend version. Hover reveals backend version + build date so
 * operators can confirm which build they are looking at.
 *
 * APP_VERSION is injected by Umi's `define` at build time. In jest
 * (and any other context where the bundle has not been processed by
 * Umi) it falls back to '0.0.0' so the badge still renders.
 */
export default function VersionBadge() {
  const version =
    (typeof process !== 'undefined' && process.env?.APP_VERSION) || '0.0.0';

  return (
    <Tooltip
      title={
        <>
          <div>Material LIMS Frontend</div>
          <div>Backend {BACKEND_VERSION}</div>
          <div>Build {BUILD_DATE}</div>
        </>
      }
    >
      <Tag color="blue" style={{ cursor: 'default', margin: 0 }}>
        v{version}
      </Tag>
    </Tooltip>
  );
}
