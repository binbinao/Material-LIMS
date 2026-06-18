// jest.setup.ts — runs after the test framework is installed, before
// any test. Registers @testing-library/jest-dom's custom matchers
// (toBeInTheDocument, toHaveTextContent, etc.) globally.
//
// Issue #10: this file is referenced from jest.config.js so a single
// import here covers every *.test.ts(x) in the project.

import '@testing-library/jest-dom';
