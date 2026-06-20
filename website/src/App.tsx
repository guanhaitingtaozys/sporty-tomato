import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Features from './components/Features';
import Screenshots from './components/Screenshots';
import DownloadSection from './components/Download';
import Footer from './components/Footer';

export default function App() {
  return (
    <div className="min-h-screen bg-[#0a0f0d] text-white overflow-x-hidden">
      <Navbar />
      <Hero />
      <Features />
      <Screenshots />
      <DownloadSection />
      <Footer />
    </div>
  );
}
