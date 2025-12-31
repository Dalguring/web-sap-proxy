import {BrowserRouter, Routes, Route, Link} from 'react-router-dom';
import InterfaceList from './pages/InterfaceList';
import InterfaceEditor from './pages/InterfaceEditor';

function App() {
    return (
            <BrowserRouter>
                <div className="min-h-screen bg-gray-100 flex flex-col">
                    {/* 상단 네비게이션 바 */}
                    <header className="bg-blue-600 text-white shadow-md">
                        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
                            <h1 className="text-xl font-bold">
                                <Link to="/">SAP Proxy Manager</Link>
                            </h1>
                            <nav>
                                <Link to="/"
                                      className="px-3 py-2 hover:bg-blue-700 rounded">목록</Link>
                                <Link to="/new" className="px-3 py-2 hover:bg-blue-700 rounded">신규
                                    생성</Link>
                            </nav>
                        </div>
                    </header>

                    {/* 메인 컨텐츠 영역 */}
                    <main className="flex-grow container mx-auto px-4 py-6">
                        <Routes>
                            <Route path="/" element={<InterfaceList/>}/>
                            <Route path="/new" element={<InterfaceEditor/>}/>
                            <Route path="/edit/:id" element={<InterfaceEditor/>}/>
                        </Routes>
                    </main>
                </div>
            </BrowserRouter>
    );
}

export default App;