import { Zap, Leaf, Trophy, Users } from 'lucide-react';

const features = [
  {
    icon: Zap,
    title: '步数换能量',
    description: '每天走路就能赚能量，每 10 步 = 1 点能量，日上限 2000 点。',
    color: '#ff4d4f',
  },
  {
    icon: Leaf,
    title: '浇水种番茄',
    description: '消耗能量给番茄浇水，从种子到成熟，收获满满的成就感。',
    color: '#4caf50',
  },
  {
    icon: Users,
    title: '好友互动',
    description: '添加好友、查看排行榜、互相浇水，一起养成运动习惯。',
    color: '#2196f3',
  },
  {
    icon: Trophy,
    title: '勋章成就',
    description: '收获番茄、积累能量、结交好友，解锁各种成就勋章。',
    color: '#ff9800',
  },
];

export default function Features() {
  return (
    <section id="features" className="py-20 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">核心功能</h2>
          <p className="text-[#a8a6a3] max-w-xl mx-auto">一切从走路开始，用运动浇灌你的番茄</p>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, i) => (
            <div key={i} className="glass rounded-2xl p-6 hover:bg-white/5 transition-all group">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style={{ backgroundColor: feature.color + '20' }}>
                <feature.icon className="w-6 h-6" style={{ color: feature.color }} />
              </div>
              <h3 className="text-lg font-semibold mb-2">{feature.title}</h3>
              <p className="text-sm text-[#a8a6a3] leading-relaxed">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
