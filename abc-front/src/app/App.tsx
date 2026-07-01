import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import '../styles/variables.css';
import '../styles/base.css';
import '../styles/layout.css';

export default function App() {
  return <RouterProvider router={router} />;
}
