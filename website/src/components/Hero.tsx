import { Download } from 'lucide-react';

export default function Hero() {
  return (
    <section className="min-h-screen flex items-center justify-center relative overflow-hidden pt-16">
      <div className="absolute inset-0 bg-grid opacity-60" />
      <div className="absolute top-1/4 left-1/4 w-72 h-72 bg-[#ff4d4f]/20 rounded-full blur-[100px]" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#1b5e20]/20 rounded-full blur-[120px]" />
      <div className="relative z-10 max-w-5xl mx-auto px-4 text-center">
        <div className="inline-flex items-center gap-2 bg-white/5 border border-white/10 rounded-full px-4 py-1.5 mb-8">
          <span className="w-2 h-2 bg-[#4caf50] rounded-full animate-pulse" />
          <span className="text-sm text-[#a8a6a3]">运动 · 健康 · 公益</span>
        </div>
        <h1 className="text-5xl md:text-7xl font-bold mb-6 leading-tight">
          每一步都算< span className="text-gradient">「树」</span>
        </h1>
        <p className="text-lg md:text-xl text-[#a8a6a3] max-w-2xl mx-auto mb-10 leading-relaxed">
          把你的日常步数变成番茄的能量，每走 10 步就能收获 1 点能量。
          浇水养番茄、收获做公益，让运动充满意义。
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a href="#download" className="bg-[#ff4d4f] hover:bg-[#e04345] text-white px-8 py-3.5 rounded-full text-lg font-medium transition-all hover:scale-105 inline-flex items-center gap-2">
            <Download className="w-5 h-5" />
            下载 APK
          </a>
          <a href="#features" className="bg-white/5 hover:bg-white/10 border border-white/20 text-white px-8 py-3.5 rounded-full text-lg font-medium transition-all inline-flex items-center gap-2">
            了解更多
          </a>
        </div>
        <div className="mt-16 grid grid-cols-3 gap-8 max-w-lg mx-auto">
          {[
            { value: '5000+', label: '日活用户' },
            { value: '10万+', label: '番茄收获' },
            { value: '1000+', label: '公益活动' },
          ].map(stat => (
            <div key={stat.label}>
              <div className="text-2xl md:text-3xl font-bold text-gradient">{stat.value}</div>
              <div className="text-sm text-[#a8a6a3] mt-1">{stat.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
