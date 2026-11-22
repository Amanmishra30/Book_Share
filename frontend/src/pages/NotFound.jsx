import { Link } from 'react-router-dom';
import { Empty } from '../components/ui.jsx';

export default function NotFound() {
  return <Empty icon="🧭" title="This path leads nowhere">The signpost points to a story that is not shelved here. <Link to="/">Go back to the clearing</Link>.</Empty>;
}
