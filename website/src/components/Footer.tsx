import { Leaf } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="border-t border-white/10 py-8 px-4">
      <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Leaf className="w-5 h-5 text-[#ff4d4f]" />
          <span className="text-sm font-medium">运动的番茄</span>
        </div>
        <p className="text-sm text-[#a8a6a3]">© 2026 运动的番茄. All rights reserved.</p>
      </div>
    </footer>
  );
}
