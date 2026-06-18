// Issue #10: smoke test for the Jest + ts-jest + jsdom + @testing-library
// harness. Renders a tiny component using @testing-library/react and
// asserts the jest-dom matcher `toBeInTheDocument` works — proving the
// setup file is loaded.

import React from 'react';
import { render, screen } from '@testing-library/react';

function Greeting({ name }: { name: string }) {
  return <h1>Hello, {name}</h1>;
}

describe('Jest + Testing Library harness', () => {
  test('renders a component and resolves a jest-dom matcher', () => {
    render(<Greeting name="Material LIMS" />);
    const heading = screen.getByRole('heading', { name: /Hello, Material LIMS/i });
    expect(heading).toBeInTheDocument();
  });
});
