import { Leaf, Menu, X } from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/utils';

const navLinks = [
  { label: '功能', href: '#features' },
  { label: '预览', href: '#screenshots' },
  { label: '下载', href: '#download' },
];

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 glass">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <a href="#" className="flex items-center gap-2">
            <Leaf className="w-6 h-6 text-[#ff4d4f]" />
            <span className="text-xl font-bold">运动的番茄</span>
          </a>
          <div className="hidden md:flex items-center gap-8">
            {navLinks.map(link => (
              <a key={link.href} href={link.href} className="text-[#a8a6a3] hover:text-white transition-colors">{link.label}</a>
            ))}
            <a href="#download" className="bg-[#ff4d4f] hover:bg-[#e04345] text-white px-5 py-2 rounded-full text-sm font-medium transition-colors">立即下载</a>
          </div>
          <button className="md:hidden text-white" onClick={() => setIsOpen(!isOpen)}>
            {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>
      {isOpen && (
        <div className="md:hidden glass border-t border-white/10">
          <div className="px-4 py-4 space-y-3">
            {navLinks.map(link => (
              <a key={link.href} href={link.href} className="block text-[#a8a6a3] hover:text-white transition-colors py-2" onClick={() => setIsOpen(false)}>{link.label}</a>
            ))}
            <a href="#download" className="block text-center bg-[#ff4d4f] text-white px-5 py-2 rounded-full text-sm font-medium">立即下载</a>
          </div>
        </div>
      )}
    </nav>
  );
}
