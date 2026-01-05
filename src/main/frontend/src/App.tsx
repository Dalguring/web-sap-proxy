import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import InterfaceList from './pages/InterfaceList';
import InterfaceEditor from './pages/InterfaceEditor';
import LogViewer from './pages/LogViewer';

function App() {
    return (
        <BrowserRouter>
            <div className="min-h-screen bg-gray-100 text-gray-900 font-sans">
                {/* 네비게이션 바 */}
                <nav className="bg-white shadow-sm border-b border-gray-200">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                        <div className="flex justify-between h-16">
                            <div className="flex items-center gap-8">
                                <Link to="/" className="flex-shrink-0 flex items-center gap-2">
                                    <div className="w-8 h-8 bg-blue-600 rounded flex items-center justify-center text-white font-bold">P</div>
                                    <span className="font-bold text-xl text-gray-800">SAP Proxy Admin</span>
                                </Link>

                                <div className="hidden sm:flex sm:space-x-8">
                                    <Link to="/" className="text-gray-900 inline-flex items-center px-1 pt-1 border-b-2 border-transparent hover:border-blue-500 font-medium">
                                        인터페이스 관리
                                    </Link>
                                    {/* [추가] 로그 통계 메뉴 */}
                                    <Link to="/logs" className="text-gray-900 inline-flex items-center px-1 pt-1 border-b-2 border-transparent hover:border-blue-500 font-medium">
                                        로그 통계
                                    </Link>
                                </div>
                            </div>
                        </div>
                    </div>
                </nav>

                {/* 메인 컨텐츠 */}
                <main className="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">
                    <Routes>
                        <Route path="/" element={<InterfaceList />} />
                        <Route path="/new" element={<InterfaceEditor />} />
                        <Route path="/edit/:id" element={<InterfaceEditor />} />
                        <Route path="/logs" element={<LogViewer />} />
                    </Routes>
                </main>
            </div>
        </BrowserRouter>
    );
}

export default App;