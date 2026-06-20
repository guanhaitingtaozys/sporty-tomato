import { Download, Smartphone, Shield, Cloud } from 'lucide-react';

export default function Download() {
  return (
    <section id="download" className="py-20 px-4">
      <div className="max-w-6xl mx-auto text-center">
        <div className="inline-flex items-center gap-2 bg-white/5 border border-white/10 rounded-full px-4 py-1.5 mb-6">
          <span className="w-2 h-2 bg-[#4caf50] rounded-full animate-pulse" />
          <span className="text-sm text-[#a8a6a3]">Android 版本</span>
        </div>
        <h2 className="text-3xl md:text-4xl font-bold mb-4">立即下载</h2>
        <p className="text-[#a8a6a3] max-w-xl mx-auto mb-10">
          运动的番茄 Android APK，直接安装即可使用
        </p>
        <a
          href="sporty-tomato.apk"
          className="inline-flex items-center gap-3 bg-[#ff4d4f] hover:bg-[#e04345] text-white px-10 py-4 rounded-full text-lg font-medium transition-all hover:scale-105 mb-12"
        >
          <Download className="w-6 h-6" />
          下载 APK（v2.0）
        </a>
        <div className="grid md:grid-cols-3 gap-6 max-w-3xl mx-auto">
          {[
            { icon: Smartphone, title: '兼容性强', desc: '支持 Android 9+，适配各类机型' },
            { icon: Shield, title: '隐私安全', desc: '本地优先存储，云端同步可选' },
            { icon: Cloud, title: '云端同步', desc: '数据云端备份，换机不丢失' },
          ].map((item, i) => (
            <div key={i} className="glass rounded-xl p-6">
              <item.icon className="w-8 h-8 mx-auto mb-3 text-[#4caf50]" />
              <h4 className="font-semibold mb-1">{item.title}</h4>
              <p className="text-sm text-[#a8a6a3]">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
