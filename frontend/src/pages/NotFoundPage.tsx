import { Link } from "react-router-dom";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import "./not-found.css";

export default function NotFoundPage() {
    return (
        <main className="not-found">
            <div className="not-found__layout">
                <div className="not-found__media">
                    <img
                        className="not-found__image"
                        src="/not-found-dog-cropped.png"
                        alt="A blue line-art dog looking a little guilty"
                    />
                </div>

                <div className="not-found__content">
                    <h1 className="not-found__title">
                        <span className="not-found__title-oopsie">Oopsie</span>
                        <span className="not-found__title-poopsie">Poopsie</span>
                        <span className="not-found__title-code">404</span>
                    </h1>

                    <p className="not-found__message">
                        Looks like this page slipped off the onboarding path. No worries,
                        let&apos;s get you back to the learning library.
                    </p>

                    <Link className="not-found__action" to="/library">
                        <ArrowBackRoundedIcon />
                        Back to library
                    </Link>
                </div>
            </div>
        </main>
    );
}
