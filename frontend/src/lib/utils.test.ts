import { cn } from './utils';

describe('utils', () => {
  describe('cn', () => {
    it('merges tailwind classes correctly', () => {
      expect(cn('p-4', 'p-8')).toBe('p-8');
      expect(cn('p-4', undefined, null, 'bg-red-500', { 'text-white': true })).toBe('p-4 bg-red-500 text-white');
    });
  });
});
