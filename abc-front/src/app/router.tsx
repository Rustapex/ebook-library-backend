import { createBrowserRouter } from 'react-router-dom';
import { AdminLayout } from '../components/layout/AdminLayout';
import { ProtectedRoute } from '../components/layout/ProtectedRoute';
import { UserLayout } from '../components/layout/UserLayout';
import { AdminDashboardPage } from '../pages/admin/A001Dashboard/AdminDashboardPage';
import { AdminMemberListPage } from '../pages/admin/A002Members/AdminMemberListPage';
import { AdminMemberDetailPage } from '../pages/admin/A003MemberDetail/AdminMemberDetailPage';
import { AdminBookListPage } from '../pages/admin/A004Books/AdminBookListPage';
import { AdminBookCreatePage } from '../pages/admin/A005BookCreate/AdminBookCreatePage';
import { AdminBookEditPage } from '../pages/admin/A006BookEdit/AdminBookEditPage';
import { AdminCategoryPage } from '../pages/admin/A007Categories/AdminCategoryPage';
import { AdminRentalListPage } from '../pages/admin/A008Rentals/AdminRentalListPage';
import { AdminPaymentListPage } from '../pages/admin/A009Payments/AdminPaymentListPage';
import { AdminReportListPage } from '../pages/admin/A010Reports/AdminReportListPage';
import { AdminBookRequestPage } from '../pages/admin/A011BookRequests/AdminBookRequestPage';
import { AdminNoticePage } from '../pages/admin/A012Notices/AdminNoticePage';
import { AdminCouponsPointsPage } from '../pages/admin/A013CouponsPoints/AdminCouponsPointsPage';
import { AdminChallengePage } from '../pages/admin/A014Challenges/AdminChallengePage';
import { AdminLibraryPage } from '../pages/admin/A015Libraries/AdminLibraryPage';
import { AdminStatisticsPage } from '../pages/admin/A016Statistics/AdminStatisticsPage';
import { AdminAuditLogPage } from '../pages/admin/A017AuditLogs/AdminAuditLogPage';
import { AdminCollectionPage } from '../pages/admin/A018Collections/AdminCollectionPage';
import { BookDetailPage } from '../pages/user/U008BookDetail/BookDetailPage';
import { BooksPage } from '../pages/user/U006Books/BooksPage';
import { HomePage } from '../pages/user/U001Home/HomePage';
import { LoginPage } from '../pages/user/U003Login/LoginPage';
import { MyPage } from '../pages/user/U014MyPage/MyPage';
import { SearchResultsPage } from '../pages/user/U007SearchResults/SearchResultsPage';
import { SignupPage } from '../pages/user/U002Signup/SignupPage';

export const router = createBrowserRouter([
  {
    element: <UserLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/books', element: <BooksPage /> },
      { path: '/search', element: <SearchResultsPage /> },
      { path: '/books/:bookId', element: <BookDetailPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/signup', element: <SignupPage /> },
      {
        path: '/me',
        element: (
          <ProtectedRoute>
            <MyPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: 'members', element: <AdminMemberListPage /> },
      { path: 'members/:memberId', element: <AdminMemberDetailPage /> },
      { path: 'books', element: <AdminBookListPage /> },
      { path: 'books/new', element: <AdminBookCreatePage /> },
      { path: 'books/:bookId/edit', element: <AdminBookEditPage /> },
      { path: 'categories', element: <AdminCategoryPage /> },
      { path: 'rentals', element: <AdminRentalListPage /> },
      { path: 'payments', element: <AdminPaymentListPage /> },
      { path: 'reports', element: <AdminReportListPage /> },
      { path: 'book-requests', element: <AdminBookRequestPage /> },
      { path: 'notices', element: <AdminNoticePage /> },
      { path: 'coupons-points', element: <AdminCouponsPointsPage /> },
      { path: 'challenges', element: <AdminChallengePage /> },
      { path: 'libraries', element: <AdminLibraryPage /> },
      { path: 'statistics', element: <AdminStatisticsPage /> },
      { path: 'audit-logs', element: <AdminAuditLogPage /> },
      { path: 'collections', element: <AdminCollectionPage /> },
    ],
  },
]);
