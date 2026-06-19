// jest.config.js — Material LIMS frontend unit tests
// Issue #10: provide a ts-jest + jsdom config so `npm test` actually
// runs the test files under src/**/__tests__/.
// Issue #21: fix `setupFilesAfterEach` (typo) → `setupFilesAfterEnv`
// (the real Jest option) so jest.setup.ts is actually loaded and the
// jest-dom matchers are registered before each test.

module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  transform: {
    '^.+\\.(ts|tsx)$': ['ts-jest', { tsconfig: 'tsconfig.json' }],
  },
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  testMatch: ['<rootDir>/src/**/__tests__/**/*.test.(ts|tsx)'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
};