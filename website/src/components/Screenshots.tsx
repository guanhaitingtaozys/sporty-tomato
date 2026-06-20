export default function Screenshots() {
  const screenshots = [
    {
      title: '番茄成长',
      description: '每日步数转换能量，浇水让番茄从种子到成熟',
      emoji: '🍅',
      accent: '#ff4d4f',
    },
    {
      title: '好友排行榜',
      description: '和好友比拼收获次数，互相激励',
      emoji: '🏆',
      accent: '#ff9800',
    },
    {
      title: '勋章墙',
      description: '各种成就等你解锁，记录你的运动之旅',
      emoji: '🎖️',
      accent: '#4caf50',
    },
  ];

  return (
    <section id="screenshots" className="py-20 px-4 bg-white/[0.02]">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">应用预览</h2>
          <p className="text-[#a8a6a3]">简洁美观的界面，流畅的交互体验</p>
        </div>
        <div className="grid md:grid-cols-3 gap-8">
          {screenshots.map((item, i) => (
            <div key={i} className="glass rounded-2xl p-8 text-center hover:bg-white/5 transition-all group">
              <div className="text-6xl mb-6 inline-block group-hover:scale-110 transition-transform">{item.emoji}</div>
              <h3 className="text-xl font-semibold mb-3" style={{ color: item.accent }}>{item.title}</h3>
              <p className="text-sm text-[#a8a6a3] leading-relaxed">{item.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
